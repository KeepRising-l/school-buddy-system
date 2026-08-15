import request from './request'

export const getAllTags = () => request.get('/api/tag/list', { silent: true })
// 获取所有分类名称（广场筛选/发帖分类动态加载用，V1.2）
export const getAllCategories = () => request.get('/api/tag/categories', { silent: true })
