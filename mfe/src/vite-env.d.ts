/// <reference types="vite/client" />

// 声明 PDF 文件模块
declare module '*.pdf' {
  const src: string;
  export default src;
}

declare module '*.pdf?url' {
  const src: string;
  export default src;
}
