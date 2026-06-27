import {
  DEFAULT_ROUTE_PRIORITY,
  ROUTE_PERMISSIONS,
} from '../constants/navigation/navItems'

export function filterNavItems(items, can) {
  return items.reduce((visible, item) => {
    if (item.children?.length) {
      const children = item.children.filter(
        (child) => !child.permission || can(child.permission),
      )
      if (children.length > 0) {
        visible.push({ ...item, children })
      }
      return visible
    }

    if (!item.permission || can(item.permission)) {
      visible.push(item)
    }

    return visible
  }, [])
}

export function getDefaultRoute(permissions = []) {
  const permissionSet = new Set(permissions)

  for (const path of DEFAULT_ROUTE_PRIORITY) {
    const required = ROUTE_PERMISSIONS[path]
    if (!required || permissionSet.has(required)) {
      return path
    }
  }

  return '/403'
}
