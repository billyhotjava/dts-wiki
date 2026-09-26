import { Alert, DatePicker, Form, Input, Select, Space, Switch, Typography } from 'antd';
import Ajv from 'ajv/dist/2020';
import dayjs from 'dayjs';
import { parseDocument, type Document } from 'yaml';
import { useEffect, useMemo, useState } from 'react';
import { api } from '../../api/client';

interface SchemaProp {
  type?: string;
  enum?: string[];
  format?: string;
  pattern?: string;
  items?: { type?: string };
  properties?: Record<string, SchemaProp>;
  required?: string[];
}

interface JsonSchema {
  properties?: Record<string, SchemaProp>;
  required?: string[];
}

// Frontmatter editing (design 10 S4.3): schema-driven antd Form + YAML source toggle.
// YAML read/write uses the `yaml` Document API: only edited keys change, key order
// and comments are preserved. Client pre-checks with ajv (2020-12); the server
// re-validates and answers 422 with field errors.
export function PropertiesForm({ initialYaml, onChange }: { initialYaml: string; onChange: (yaml: string) => void }) {
  const [yamlMode, setYamlMode] = useState(false);
  const [yamlText, setYamlText] = useState(initialYaml);
  const [schema, setSchema] = useState<JsonSchema | null>(null);
  const [clientErrors, setClientErrors] = useState<string[]>([]);

  const doc: Document | null = useMemo(() => {
    try {
      return yamlText.trim() === '' ? null : parseDocument(yamlText);
    } catch {
      return null;
    }
  }, [yamlText]);

  const docType: string = useMemo(() => {
    const value = doc?.get('type');
    return typeof value === 'string' ? value : 'page';
  }, [doc]);

  useEffect(() => {
    let cancelled = false;
    api
      .get<JsonSchema>(`/api/wiki/content-schemas/${docType}`)
      .then(({ data }) => {
        if (!cancelled) setSchema(data);
      })
      .catch(() => undefined);
    return () => {
      cancelled = true;
    };
  }, [docType]);

  const emit = (next: string) => {
    setYamlText(next);
    onChange(next);
  };

  const setField = (key: string, value: unknown) => {
    const next = doc ?? parseDocument('');
    if (value === undefined || value === null || value === '') {
      next.delete(key);
    } else {
      next.set(key, value);
    }
    // ajv pre-check (best effort; server is authoritative)
    try {
      if (schema !== null) {
        const ajv = new Ajv({ strict: false });
        const validate = ajv.compile(schema);
        const candidate = next.toJS() as Record<string, unknown>;
        if (!validate(candidate)) {
          setClientErrors((validate.errors ?? []).map(e => `${e.instancePath || '$'}: ${e.message ?? ''}`));
        } else {
          setClientErrors([]);
        }
      }
    } catch {
      // ignore client validation crashes
    }
    emit(next.toString());
  };

  const getField = (key: string): unknown => doc?.get(key);

  const renderField = (key: string, prop: SchemaProp) => {
    if (prop.enum !== undefined) {
      return (
        <Select allowClear value={(getField(key) as string) ?? undefined} onChange={v => setField(key, v)} options={prop.enum.map(e => ({ value: e, label: e }))} style={{ width: '100%' }} />
      );
    }
    if (prop.type === 'array' && prop.items?.type === 'string') {
      const value = getField(key);
      return <Select mode="tags" value={Array.isArray(value) ? (value as string[]) : []} onChange={v => setField(key, v)} style={{ width: '100%' }} />;
    }
    if (key === 'timebox' || prop.properties?.start !== undefined) {
      const value = (getField(key) as { start?: string; end?: string }) ?? {};
      return (
        <Input.Group compact>
          <DatePicker
            value={value.start ? dayjs(value.start) : null}
            onChange={d => setField(key, { ...value, start: d === null ? undefined : d.format('YYYY-MM-DD') })}
            placeholder="开始"
          />
          <DatePicker
            value={value.end ? dayjs(value.end) : null}
            onChange={d => setField(key, { ...value, end: d === null ? undefined : d.format('YYYY-MM-DD') })}
            placeholder="结束"
          />
        </Input.Group>
      );
    }
    if (prop.pattern?.includes('^[0-9]{4}') === true || prop.format === 'date') {
      const value = getField(key);
      return (
        <DatePicker
          value={typeof value === 'string' && value !== '' ? dayjs(value) : null}
          onChange={d => setField(key, d === null ? undefined : d.format('YYYY-MM-DD'))}
          style={{ width: '100%' }}
        />
      );
    }
    return <Input value={(getField(key) as string) ?? ''} onChange={e => setField(key, e.target.value)} placeholder={prop.pattern} />;
  };

  const properties = schema?.properties ?? {};
  const required = new Set(schema?.required ?? []);

  return (
    <div style={{ marginBottom: 8 }}>
      <Space direction="vertical" style={{ width: '100%' }}>
        <span>
          属性
          <Switch checkedChildren="YAML" unCheckedChildren="表单" checked={yamlMode} onChange={setYamlMode} style={{ marginLeft: 8 }} />
        </span>
        {yamlMode ? (
          <Input.TextArea rows={8} value={yamlText} onChange={e => emit(e.target.value)} style={{ fontFamily: 'monospace' }} placeholder="type: page" />
        ) : Object.keys(properties).length === 0 ? (
          <Typography.Text type="secondary">该类型无结构化属性（可在 YAML 模式下自由添加）</Typography.Text>
        ) : (
          <Form layout="vertical">
            {Object.entries(properties).map(([key, prop]) => (
              <Form.Item key={key} label={`${key}${required.has(key) ? ' *' : ''}`} style={{ marginBottom: 8 }}>
                {renderField(key, prop)}
              </Form.Item>
            ))}
          </Form>
        )}
        {clientErrors.length > 0 && <Alert type="warning" showIcon message="客户端预校验" description={clientErrors.join('；')} />}
      </Space>
    </div>
  );
}
