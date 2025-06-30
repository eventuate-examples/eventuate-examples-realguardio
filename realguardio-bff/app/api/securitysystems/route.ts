import { getServerSession } from 'next-auth';
import { authOptions } from '@/authOptions';
import { NextResponse } from "next/server";
import { getToken } from "next-auth/jwt";
import { cookies, headers } from "next/headers";
import axios from 'axios';

import { trace } from '@opentelemetry/api';

const tracer = trace.getTracer('realguardio-bff');

export async function GET() {
  const session = await getServerSession(authOptions);

  if (!session) {
    return NextResponse.json(
      { error: "Unauthorized: Please sign in to access this resource" },
      { status: 401 }
    );
  }

  const req = {
    headers: await headers(),
    cookies: await cookies(),
  };

  // @ts-ignore
  const token = await getToken({ req, secret: process.env.NEXTAUTH_SECRET });
  // @ts-ignore
  const { accessToken } = token;
  // console.log("server-side accessToken=", accessToken)
  // console.log("server-side refreshToken=", refreshToken)

  try {
    const securitySystemsApiUrl = process.env.SECURITY_SYSTEMS_API_URL || 'http://localhost:50962';
    console.log("securitySystemsApiUrl=", securitySystemsApiUrl);

    const response = await tracer.startActiveSpan('manual-span-check', async (span) => {
      const x = await axios.get(`${securitySystemsApiUrl}/securitysystems`, {
        headers: {
          'Authorization': `Bearer ${accessToken}`,
        }
      });
      span.end();
      return x
    });

    if (response.status != 200) {
      console.error('HTTP error! status', response.status);
      return NextResponse.json(
          { error: 'Failed to fetch securitySystems' },
          { status: response.status }
      );
    }

    const data = response.data;
    return NextResponse.json(data);
  } catch (error) {
    console.error('Error fetching securitySystems:', error);
    return NextResponse.json(
      { error: 'Failed to fetch securitySystems' },
      { status: 500 }
    );
  }
}
