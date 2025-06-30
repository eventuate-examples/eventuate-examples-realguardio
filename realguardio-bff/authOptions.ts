import type { NextAuthOptions } from 'next-auth';

export const authOptions: NextAuthOptions = {
  debug: process.env.NODE_ENV === 'development',

  providers: [
    {
      id: 'oauth2-pkce',
      name: 'OAuth2 PKCE Provider',
      type: 'oauth',
      clientId: process.env.OAUTH_CLIENT_ID,
      clientSecret: process.env.OAUTH_CLIENT_SECRET,

//      wellKnown: process.env.OAUTH_WELL_KNOWN_URL,

      token: process.env.OAUTH_TOKEN_URL,
      userinfo: process.env.OAUTH_USER_INFO_URL,
      issuer: process.env.OAUTH_ISSUER_URL,
      jwks_endpoint: process.env.OAUTH_JWKS_URL,

      authorization: {
        url: process.env.OAUTH_AUTHORIZATION_URL,
        params: {
          scope: 'openid',
          response_type: 'code',
          code_challenge_method: 'S256',
        },
      },
      checks: ['pkce', 'state'],
      profile(profile) {
        return {
          id: profile.sub,
          name: profile.name,
          email: profile.email,
          image: profile.picture,
        };
      },
    },
  ],
  session: {
    strategy: 'jwt',
  },
  callbacks: {
    async jwt({ token, user, account, profile }) {
      // console.log("jwt-callback", { token, user, account, profile })
      // token is defined, nothing else is
      if (account) {
        token.accessToken = account.access_token;
        token.refreshToken = account.refresh_token;
        token.idToken = account.id_token;
        token.expires_at = account.expires_at;
      }
      if (user) {
        token.user = user;
      }
      if (profile) {
        // @ts-ignore
        token.authorities = profile.authorities;
      }
      // @ts-ignore
      if (!account && !(Date.now() < token.expires_at * 1000)) {
        // From https://authjs.dev/guides/refresh-token-rotation
        console.log("********************* Token expired, refreshing... ***************");

        if (!token.refreshToken) throw new TypeError("Missing refreshToken")

        try {

          const refreshParams = {
            grant_type: "refresh_token",
            refresh_token: token.refreshToken!,
          };
          console.log("refreshParams", refreshParams);
          // @ts-ignore
          const response = await fetch(process.env.OAUTH_TOKEN_URL, {
            headers: {
              "Authorization": `Basic ${Buffer.from(`${process.env.OAUTH_CLIENT_ID}:${process.env.OAUTH_CLIENT_SECRET}`).toString('base64')}`,
              "Content-Type": "application/x-www-form-urlencoded"
            },
            method: "POST",
            // @ts-ignore
            body: new URLSearchParams(refreshParams),
          })

          const tokensOrError = await response.json()

          if (!response.ok) throw tokensOrError

          console.log("****** token refreshed ****")

          const newTokens = tokensOrError as {
            access_token: string
            expires_in: number
            refresh_token?: string
          }

          return {
            ...token,
            accessToken: newTokens.access_token,
            expires_at: Math.floor(Date.now() / 1000 + newTokens.expires_in),
            // Some providers only issue refresh tokens once, so preserve if we did not get a new one
            refreshToken: newTokens.refresh_token
                ? newTokens.refresh_token
                : token.refreshToken,
          }
        } catch (error) {
          console.error("Error refreshing access_token", error)
          // If we fail to refresh the token, return an error so we can handle it on the page
          token.error = "RefreshTokenError"
          return token
        }

      } else
        console.log("********************* Token NOT Expired ***************");
      return token;
    },
    async session({ session, token }) {
      // @ts-ignore
      session.user.name = token.name; // Ensure the name is set in the session
      // @ts-ignore
      session.authorities = token.authorities; // Add authorities to the session
      return session;
    },
  },
};