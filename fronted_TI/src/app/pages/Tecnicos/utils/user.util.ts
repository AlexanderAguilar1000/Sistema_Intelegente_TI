export const USERNAME_PATTERN = /^[a-z]+(\.[a-z]+)+$/;

export function getInitials(fullName: string): string {
  return fullName
    .split(' ')
    .filter(Boolean)
    .map((part) => part[0])
    .join('')
    .slice(0, 2)
    .toUpperCase();
}

export function toCorporateEmail(username: string): string {
  return `${username}@empresa.es`;
}
