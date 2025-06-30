// Types generated from OpenAPI specification

export type SecuritySystemState = 'DISARMED' | 'ARMED' | 'ALARMED' | 'FAULT';

export type SecuritySystemAction = 'DISARM' | 'ARM' | 'ACKNOWLEDGE';

export interface GetSecuritySystemResponse {
  id: number;
  locationName: string;
  state: SecuritySystemState;
  actions: SecuritySystemAction[];
}

export interface GetSecuritySystemsResponse {
  securitySystems: GetSecuritySystemResponse[];
}