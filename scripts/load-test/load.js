import http from 'k6/http';
import { check, sleep } from 'k6';

const API = __ENV.API || 'http://foody-load-test-api:8080';
const PASSWORD = __ENV.PASSWORD || 'Foody#2026';
const STEADY = __ENV.STEADY || '6m';
const CUSTOMERS = Number(__ENV.CUSTOMERS || 160);
const STAFF = Number(__ENV.STAFF || 40);
const PEAK_FACTOR = 1.5;

const stages = (steady) => [
  { duration: '2m', target: steady },
  { duration: STEADY, target: steady },
  { duration: '1m', target: Math.round(steady * PEAK_FACTOR) },
  { duration: '1m', target: 0 },
];

export const options = {
  setupTimeout: '120s',
  summaryTrendStats: ['avg', 'p(50)', 'p(95)', 'p(99)', 'max'],
  scenarios: {
    customers: { executor: 'ramping-vus', exec: 'customer', startVUs: 0, stages: stages(CUSTOMERS), gracefulRampDown: '20s' },
    staff: { executor: 'ramping-vus', exec: 'staff', startVUs: 0, stages: stages(STAFF), gracefulRampDown: '20s' },
  },
};

const JSON_HEADERS = { 'Content-Type': 'application/json' };
const ADDRESS = { zipCode: '01310-100', street: 'Avenida Paulista', number: '1000', neighborhood: 'Bela Vista', city: 'Sao Paulo', state: 'SP' };
const PERIODS = ['TODAY', 'LAST_7_DAYS', 'LAST_30_DAYS'];
const SEARCHES = ['ana', 'joao', 'silva', 'maria', 'pedro', 'costa', '10', ''];
const SORTS = ['placedAt,desc', 'total,desc', 'number,asc'];
const ORDER_SHARE = 0.25;

function login(email) {
  const response = http.post(`${API}/auth/login`, JSON.stringify({ email, password: PASSWORD }), { headers: JSON_HEADERS, tags: { name: 'login' } });
  check(response, { 'login 200': (r) => r.status === 200 });
  return response.json('accessToken');
}

function authed(token) {
  return { headers: { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' } };
}

function pick(list) {
  return list[Math.floor(Math.random() * list.length)];
}

export function setup() {
  const admin = authed(login(__ENV.ADMIN_EMAIL || 'admin@foody.local'));
  let emails = [];
  for (let page = 1; page <= 10; page++) {
    const data = http.get(`${API}/admin/users?role=CUSTOMER&status=ACTIVE&pageSize=100&page=${page}`, admin).json('data');
    if (!data || data.length === 0) break;
    emails = emails.concat(data.map((user) => user.email));
  }
  const menu = http.get(`${API}/menu`, authed(login(__ENV.CUSTOMER_EMAIL || 'cliente@foody.local'))).json();
  const products = menu.categories.flatMap((category) => category.products.map((product) => product.id));
  return { emails, products };
}

let token = null;

export function customer(data) {
  if (!token) token = login(data.emails[__VU % data.emails.length]);
  const options = authed(token);
  http.get(`${API}/me`, { ...options, tags: { name: 'me' } });
  http.get(`${API}/menu`, { ...options, tags: { name: 'menu' } });
  const chosen = Array.from({ length: 1 + Math.floor(Math.random() * 3) }, () => ({ productId: pick(data.products), quantity: 1 + Math.floor(Math.random() * 2) }));
  const items = Object.values(Object.fromEntries(chosen.map((item) => [item.productId, item])));
  http.post(`${API}/menu/quote`, JSON.stringify({ items }), { ...options, tags: { name: 'quote' } });
  if (Math.random() < ORDER_SHARE) {
    const placed = http.post(`${API}/my-orders`, JSON.stringify({ items, deliveryAddress: ADDRESS }), { ...options, tags: { name: 'place-order' } });
    check(placed, { 'order 201': (r) => r.status === 201 });
  }
  const list = http.get(`${API}/my-orders?pageSize=10`, { ...options, tags: { name: 'my-orders' } });
  const first = list.status === 200 ? list.json('data.0.id') : null;
  if (first) http.get(`${API}/my-orders/${first}`, { ...options, tags: { name: 'my-order' } });
  sleep(1 + Math.random() * 2);
}

export function staff() {
  if (!token) token = login(__VU % 2 === 0 ? __ENV.STAFF_EMAIL || 'equipe@foody.local' : __ENV.ADMIN_EMAIL || 'admin@foody.local');
  const options = authed(token);
  const board = http.get(`${API}/orders?status=RECEIVED&status=PREPARING&status=OUT_FOR_DELIVERY&pageSize=100&sort=placedAt,asc`, { ...options, tags: { name: 'board' } });
  check(board, { 'board 200': (r) => r.status === 200 });
  const roll = Math.random();
  if (roll < 0.3 && board.status === 200) {
    const movable = (board.json('data') || []).filter((order) => order.allowedTransitions.length > 0 && !order.hasPendingCancellationRequest);
    if (movable.length > 0) {
      const order = pick(movable);
      http.patch(`${API}/orders/${order.id}/status`, JSON.stringify({ status: order.allowedTransitions[0] }), { ...options, tags: { name: 'advance' } });
    }
  } else if (roll < 0.5) {
    http.get(`${API}/dashboard?period=${pick(PERIODS)}`, { ...options, tags: { name: 'dashboard' } });
  } else if (roll < 0.75) {
    http.get(`${API}/orders?search=${pick(SEARCHES)}&pageSize=25&sort=${pick(SORTS)}&page=${1 + Math.floor(Math.random() * 3)}`, { ...options, tags: { name: 'history' } });
  } else {
    http.get(`${API}/catalog/products?search=${pick(SEARCHES)}&pageSize=25&sort=name,asc`, { ...options, tags: { name: 'products' } });
  }
  sleep(2 + Math.random() * 3);
}
