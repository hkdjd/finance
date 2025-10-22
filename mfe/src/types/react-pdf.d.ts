declare module 'react-pdf' {
  import { FC, ReactElement } from 'react';

  export interface DocumentProps {
    file: string | File | { url: string; data: Uint8Array };
    onLoadSuccess?: (pdf: { numPages: number }) => void;
    onLoadError?: (error: Error) => void;
    loading?: ReactElement | string;
    error?: ReactElement | string;
    children?: React.ReactNode;
  }

  export interface PageProps {
    pageNumber: number;
    width?: number;
    height?: number;
    scale?: number;
    renderTextLayer?: boolean;
    renderAnnotationLayer?: boolean;
  }

  export const Document: FC<DocumentProps>;
  export const Page: FC<PageProps>;

  export const pdfjs: {
    version: string;
    GlobalWorkerOptions: {
      workerSrc: string;
    };
  };
}
