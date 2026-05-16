import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const request = axios.create({
  baseURL: '/api',
  timeout: 10000
})

// 请求拦截器：添加 token
request.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
}, error => {
  return Promise.reject(error)
})

// 响应拦截器：统一处理返回结构
request.interceptors.response.use(res => {
  const { code, message, data } = res.data
  if (code === 200) {
    return data
  } else if (code === 401) {
    localStorage.removeItem('token')
    router.push('/login')
    ElMessage.error('未登录')
    return Promise.reject(new Error(message))
  } else {
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  }
}, error => {
  ElMessage.error(error.message || '网络异常')
  return Promise.reject(error)
})

export default request
