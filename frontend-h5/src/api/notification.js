import request from './request'

// 通知列表（分页）
export const getNotifications = (params) => request.get('/api/notification/list', { params })
// 未读通知数（角标）
export const getUnreadCount = () => request.get('/api/notification/unread-count')
// 标记单条已读
export const markNotificationRead = (id) => request.put(`/api/notification/read/${id}`)
// 全部已读
export const markAllNotificationsRead = () => request.put('/api/notification/read-all')
