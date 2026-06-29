import {
  DEFAULT_ROUTE_PRIORITY,
  getDefaultRoutePriorityForUser,
  ROUTE_PERMISSIONS,
} from '../constants/navigation/navItems'

/** Only the most specific matching route should appear active. */
export function isNavItemActive(pathname, targetTo, siblings = []) {
  if (!targetTo) {
    return false
  }

  const routes = [targetTo, ...siblings.map((sibling) => sibling.to)].filter(Boolean)
  const matchingRoutes = routes.filter(
    (to) => pathname === to || pathname.startsWith(`${to}/`),
  )

  if (matchingRoutes.length === 0) {
    return false
  }

  const activeRoute = matchingRoutes.sort((a, b) => b.length - a.length)[0]
  return activeRoute === targetTo
}

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

export function collectNavRoutes(items = []) {
  return items.flatMap((item) =>
    item.to ? [item.to] : item.children?.map((child) => child.to) ?? [],
  )
}

export function getDefaultRoute(permissions = [], user = null) {
  const permissionSet = new Set(permissions)
  const priorities = user
    ? getDefaultRoutePriorityForUser(user)
    : DEFAULT_ROUTE_PRIORITY

  for (const path of priorities) {
    const required = ROUTE_PERMISSIONS[path]
    if (!required || permissionSet.has(required)) {
      return path
    }
  }

  return '/403'
}
