# Use Node.js LTS as the base image
FROM node:22 AS base

# Set working directory
WORKDIR /app

# Install dependencies only when needed
FROM base AS deps
# COPY package.json package-lock.json ./
COPY package.json ./
RUN npm install --legacy-peer-deps
# Install architecture-specific dependencies
RUN ARCH=$(uname -m | sed 's/aarch64/arm64/;s/x86_64/x64/') && \
    npm install --legacy-peer-deps @tailwindcss/oxide-linux-$ARCH-gnu \
        lightningcss-linux-$ARCH-gnu

# Rebuild the source code only when needed
FROM base AS builder
COPY --from=deps /app/node_modules ./node_modules
COPY . .
COPY --from=deps /app/package.json /app/package-lock.json .
RUN rm -f .env.local
EXPOSE 3000

# CMD ["sleep", "3600"]
CMD ["npm", "run", "dev"]