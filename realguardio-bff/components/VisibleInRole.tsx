'use client';

/**
 * VisibleInRole is a component that conditionally renders its children based on user roles.
 * It checks if the user has the required role in their session authorities.
 * 
 * @example
 * ```tsx
 * // Only users with ADMIN role will see this content
 * <VisibleInRole requiredRole="ADMIN">
 *   <div>Admin only content</div>
 * </VisibleInRole>
 * 
 * // Only users with USER role will see this content
 * <VisibleInRole requiredRole="USER">
 *   <div>User only content</div>
 * </VisibleInRole>
 * ```
 */

import { useSession } from 'next-auth/react';
import { ReactNode } from 'react';

interface VisibleInRoleProps {
  /** The role required to view the content */
  requiredRole: string;
  /** The content to be conditionally rendered */
  children: ReactNode;
}

export default function VisibleInRole({ requiredRole, children }: VisibleInRoleProps) {
  const { data: session } = useSession();

  // If there's no session or no authorities, don't render the children
  // @ts-ignore
  if (!session?.authorities) {
    return null;
  }

  // Check if the required role exists in the authorities array
  // @ts-ignore
  const hasRole = session.authorities.includes(requiredRole);

  // Only render children if the user has the required role
  return hasRole ? <>{children}</> : null;
}
