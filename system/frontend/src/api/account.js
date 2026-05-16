import request from './request'

export function getAccountList() {
  return request.get('/account')
}

export function createAccount(data) {
  return request.post('/account', data)
}

export function updateAccount(id, data) {
  return request.put(`/account/${id}`, data)
}

export function deleteAccount(id) {
  return request.delete(`/account/${id}`)
}

export function getAccountBalance() {
  return request.get('/account/balance')
}
