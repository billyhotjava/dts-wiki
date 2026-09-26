import { createInstance } from 'i18next';
import { initReactI18next } from 'react-i18next';
import en from './locales/en/common.json';
import zhCN from './locales/zh-CN/common.json';

// UI strings via i18n, zh-CN default (repo convention).
export const i18n = createInstance();
void i18n.use(initReactI18next).init({
  resources: { 'zh-CN': { translation: zhCN }, en: { translation: en } },
  lng: 'zh-CN',
  fallbackLng: 'en',
  interpolation: { escapeValue: false },
});
