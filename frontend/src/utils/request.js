import axios from "axios";
import router from "@/router/index.js";
import { ElMessage } from "element-plus";

// 创建axios
const request = axios.create({
  // 根请求地址
  baseURL: "http://127.0.0.1:8080",
  withCredentials: false, // 请求接口跨域时是否需要凭证
  timeout: 10000, // 超时时间，单位毫秒
});

// 配置请求头的参数类型，和编码格式
axios.defaults.headers["Content-Type"] = "application/json;charset=utf-8";

// 配置响应拦截器
request.interceptors.response.use(
  (response) => {
    // 后端返回格式可能是：{code: 200, data, message} 或 {success: true, data, ...}
    let res = response.data;
    
    // 如果响应没有 code 字段，可能是 success 格式，直接返回
    if (res.code == null) {
      // 检查是否是 success 格式
      if (res.success === true) {
        return response;
      }
      // 请求未发送到后端，直接返回
      return response;
    } else if (res.code == 200) {
      return response;
    } else if (res.code == 403) {
      // 需要重新登陆，跳转到登录页面，清除pinia中的数据，sessionStorage中
      window.localStorage.clear();
      router.push("/login");
      return Promise.reject(res.message || "未授权");
    }
    ElMessage.error(res.message || "请求失败");
    return Promise.reject(res.message);
  },
  (error) => {
    // 出现异常
    const message = error.response?.data?.message || error.message || "网络错误";
    ElMessage.error(message);
    if (error.response?.status === 401 || error.response?.status === 403) {
      window.localStorage.clear();
      router.push("/login");
    }
    return Promise.reject(error);
  }
);

// 导出
export default request;
