# 悦享便利 · 品牌官网 & H5

Vue 3 + Vite + TypeScript + vue-router + axios，自有 CSS 设计系统，无 UI 组件库。

## 启动

```bash
npm install
npm run dev
```

访问 `http://localhost:5174`。`/api` 与 `/uploads` 已代理到 `http://127.0.0.1:8080`。

## 租户

默认租户 `yxbl`，可用 URL 参数覆盖：`http://localhost:5174/?tenant=yxbl`。

## 页面

- PC：`/`、`/stores`、`/products`、`/franchise`、`/news`、`/news/:id`、`/contact`
- H5：`/h5/deals`、`/h5/newcomer`、`/h5/franchise`、`/h5/product/:skuId`、`/h5/store/:storeId`、`/h5/pay-return`

## 构建

```bash
npm run build
```
