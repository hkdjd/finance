import axios, { AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';

// 生产环境后端地址
const baseURL = 'http://localhost:8081';

const axiosInstance = axios.create({
  baseURL,
  timeout: 15000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 请求拦截器：附带 token 等通用请求头
axiosInstance.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = typeof window !== 'undefined' ? localStorage.getItem('token') : null;
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);
// 响应拦截器：统一提取数据，并处理错误
axiosInstance.interceptors.response.use(
  (response: AxiosResponse) => {
    // 约定优先返回后端 data 字段
    return response.data;
  },
  (error) => {
    if (error?.response) {
      // 后端返回了错误响应
      console.warn('[API ERROR]', error.response.status, error.response.data);
    } else {
      // 网络错误或无响应
      console.warn('[API ERROR] Network', error?.message || error);
    }
    return Promise.reject(error);
  }
);

// 便捷方法（可选），统一用法，方便类型推导
export const apiGet = <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
  axiosInstance.get<T>(url, { params, ...(config || {}) });

export const apiPost = <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
  axiosInstance.post<T>(url, data, config);

export const apiPut = <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
  axiosInstance.put<T>(url, data, config);

export const apiDelete = <T = any>(url: string, config?: AxiosRequestConfig) =>
  axiosInstance.delete<T>(url, config);

export default axiosInstance;
