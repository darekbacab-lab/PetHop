import { api } from './api.js';

export function listAffiliates() { return api('/affiliates'); }
