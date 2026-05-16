import request from './request'

export function getMonthlySummary(params) {
  return request.get('/statistics/monthly', { params })
}

export function getYearlySummary(params) {
  return request.get('/statistics/yearly', { params })
}

export function getCategorySummary(params) {
  return request.get('/statistics/category-summary', { params })
}

export function getTrend(params) {
  return request.get('/statistics/trend', { params })
}
