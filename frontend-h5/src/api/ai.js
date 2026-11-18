import request from './request'

export const parsePostByAi = (prompt) => request.post('/api/ai/parse-post', { prompt })
export const polishDescByAi = (data) => request.post('/api/ai/polish-desc', data)
export const getIcebreaker = (postId) => request.get(`/api/ai/icebreaker/${postId}`, { silent: true })
