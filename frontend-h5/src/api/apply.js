import request from './request'

// 报名/候补失败由页面用 Dialog 展示具体原因，故标记 silent 跳过拦截器的 toast
export const applyJoin = (postId) => request.post('/api/apply/join', { postId }, { silent: true })
export const waitlistJoin = (postId) => request.post('/api/apply/waitlist', { postId }, { silent: true })
export const cancelApply = (id) => request.post(`/api/apply/cancel/${id}`)
export const getApplyList = (postId) => request.get(`/api/apply/list/${postId}`)
export const approveApply = (id) => request.post(`/api/apply/approve/${id}`)
export const rejectApply = (id) => request.post(`/api/apply/reject/${id}`)
export const getReceivedApplies = (params) => request.get('/api/apply/received', { params })
export const markAttendance = (id, attended) => request.post(`/api/apply/attendance/${id}`, { attended })
