import type { ThemeConfig } from 'antd';

// DTS Wiki theme (design 05 S8): primary green, rounded corners,
// PingFang/YaHei-first font stack. Dark mode via theme.darkAlgorithm (W4+).
export const theme: ThemeConfig = {
  token: {
    colorPrimary: '#2f6f5e',
    borderRadius: 6,
    fontFamily: `-apple-system, "PingFang SC", "Microsoft YaHei", sans-serif`,
  },
};
