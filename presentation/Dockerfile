FROM node:22.22-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM dependencies AS source
COPY . .

FROM source AS verification
RUN npm run verify

FROM nginx:1.27-alpine AS production
RUN apk add --no-cache jq
COPY --from=verification /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY docker-entrypoint.d/40-write-runtime-config.sh /docker-entrypoint.d/40-write-runtime-config.sh
RUN chmod +x /docker-entrypoint.d/40-write-runtime-config.sh

ENV RECONX_DEMO_URL=http://localhost:5173

EXPOSE 80
HEALTHCHECK --interval=10s --timeout=3s --retries=5 \
  CMD wget --spider -q http://127.0.0.1/healthz || exit 1
