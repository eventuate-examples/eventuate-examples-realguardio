import {GetSecuritySystemResponse, GetSecuritySystemsResponse} from "./api"


export const oaklandSystem : GetSecuritySystemResponse = {
  id: 1,
  locationName: "Oakland office",
  state: 'ARMED',
  actions: ['DISARM']
};

export const berkeleySystem : GetSecuritySystemResponse = {
  id: 2,
  locationName: "Berkeley office",
  state: 'DISARMED',
  actions: ['ARM']
};

export const haywardSystem : GetSecuritySystemResponse = {
  id: 3,
  locationName: "Hayward office",
  state: 'ALARMED',
  actions: ['DISARM', "ACKNOWLEDGE"]
};

export const sample_getSecuritySystems : GetSecuritySystemsResponse = {
    securitySystems: [
      oaklandSystem,
      berkeleySystem,
      haywardSystem
    ]
  }

  export const sample_getSecuritySystems_single : GetSecuritySystemsResponse = {
    securitySystems: [haywardSystem]
  }
