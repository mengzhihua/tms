export const fmt = (value) =>
  value ? String(value).replace('T', ' ').substring(0, 19) : ''

export const today = () => new Date().toISOString().substring(0, 10)

export const percent = (value) =>
  value === null || value === undefined ? '-' : `${Number(value).toFixed(1)}%`
