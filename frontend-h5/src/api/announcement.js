import request from './request'

export const getLatestPopup = () => request.get('/api/announcement/latest-popup', { silent: true })
export const getActiveAnnouncements = () => request.get('/api/announcement/active-list', { silent: true })
