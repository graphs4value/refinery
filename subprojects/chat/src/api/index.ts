/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

import express from 'express';

import { setupAPIClients, sseHandler } from '../middlewares';

import textToModel from './textToModel';

const router = express.Router();

router.use(setupAPIClients);

router.use(sseHandler);

router.post('/textToModel', textToModel);

export default router;
