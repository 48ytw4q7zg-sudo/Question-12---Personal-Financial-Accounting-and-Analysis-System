import request from './request'

export function getTransactionList(params) {
  return request.get('/transaction', { params })
}

export function createTransaction(data) {
  return request.post('/transaction', data)
}

export function updateTransaction(id, data) {
  return request.put(`/transaction/${id}`, data)
}

export function transfer(data) {
  return request.post('/transaction/transfer', data)
}

export function importCsv(formData) {
  return request.post('/transaction/import', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}
