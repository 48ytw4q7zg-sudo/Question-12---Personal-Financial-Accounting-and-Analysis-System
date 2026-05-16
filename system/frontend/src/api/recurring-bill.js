import request from './request'

export function getRecurringBillList() {
  return request.get('/recurring-bill')
}

export function createRecurringBill(data) {
  return request.post('/recurring-bill', data)
}

export function updateRecurringBill(id, data) {
  return request.put(`/recurring-bill/${id}`, data)
}

export function deleteRecurringBill(id) {
  return request.delete(`/recurring-bill/${id}`)
}

export function generateRecurringBill(id) {
  return request.post(`/recurring-bill/${id}/generate`)
}
