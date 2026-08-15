import request from './request'

// 提交举报
export const submitReport = (data) => request.post('/api/report/submit', data)
// 我的举报记录
export const getMyReports = (params) => request.get('/api/report/my', { params })
