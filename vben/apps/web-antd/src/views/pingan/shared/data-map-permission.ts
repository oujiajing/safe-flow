const pinganDataMapRoleCodes = new Set([
  'ADMIN',
  'CURTAIN_WALL_LEADER',
  'GROUP_LEADER',
  'ENTERPRISE_LEADER',
  'COMPANY_LEADER',
]);

export function canShowPinganDataMap(roles: readonly string[] | undefined) {
  return (roles ?? []).some((role) => pinganDataMapRoleCodes.has(role));
}
