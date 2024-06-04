import express from 'express';
import authRouter from './app/routes/auth.routes.js';

const app = express();
const port = 8080;

app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.use(authRouter);

app.listen(port, () => {
  console.log(`Flavor Finder listening on port ${port}`);
});
