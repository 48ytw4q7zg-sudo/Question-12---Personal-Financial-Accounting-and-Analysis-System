import request from './request'

export function login(data) {
  return request.post('/user/login', data)
}

export function register(data) {
  return request.post('/user/register', data)
}

export function changePassword(data) {
  return request.post('/user/change-password', data)
}
