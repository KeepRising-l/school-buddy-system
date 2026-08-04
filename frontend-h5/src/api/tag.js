import request from './request'

export const getAllTags = () => request.get('/api/tag/list', { silent: true })
