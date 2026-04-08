FROM node:20-alpine AS build
WORKDIR /workspace

COPY package*.json ./
RUN npm ci

COPY angular.json tsconfig*.json ./
COPY public public
COPY src src

RUN npm run build

FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /workspace/dist/personal-habit-tracker-web/browser /usr/share/nginx/html

EXPOSE 80
