import { registerOTel } from '@vercel/otel'

export function register() {
  registerOTel({ serviceName: 'next-app',
    instrumentationConfig: {
      fetch: {
        propagateContextUrls: [
          /.*/,
          /realguardio-security-system-service(:3001)?\/?/,  // exact service name and optional port
          /realguardio-iam-service/
        ]
      }
    }
  })

}
