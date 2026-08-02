# ============================================================================
# TICKET-ADV147 — React multi-stage build (npm -> nginx)
# ============================================================================
FROM node:22-alpine AS dependencies
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci --no-audit --no-fund

FROM dependencies AS source
COPY . .

FROM source AS test
CMD ["npm", "run", "verify"]

FROM source AS build
RUN npm run build

FROM nginx:1.27-alpine AS production
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
