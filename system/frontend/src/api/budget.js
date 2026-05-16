import request from './request'

export function getBudgetList(params) {
  return request.get('/budget', { params })
}

export function saveBudget(data) {
  return request.post('/budget', data)
}

export function getBudgetProgress(params) {
  return request.get('/budget/progress', { params })
}

export function getBudgetAlert(params) {
  return request.get('/budget/alert', { params })
}
