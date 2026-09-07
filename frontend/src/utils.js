export const fmt = (value) => {
  if (!value) {
    return ''
  }
  const text = String(value).replace('T', ' ')
  return text.length >= 19 ? text.substring(0, 19) : text
}

export const today = () => new Date().toISOString().substring(0, 10)

export const percent = (value) =>
  value === null || value === undefined ? '-' : `${Number(value).toFixed(1)}%`
