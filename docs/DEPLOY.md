# Hosting a free live demo

This guide puts RecurIQ online at no cost, in three parts:

| Part | Where | What it gives you |
|---|---|---|
| Database | [Aiven](https://aiven.io) free MySQL | Real MySQL 8, 1 GB, no credit card, doesn't expire |
| Backend | [Render](https://render.com) free web service | Runs `backend/Dockerfile`, 512 MB RAM |
| Frontend | [Vercel](https://vercel.com) | Serves the React app and forwards `/api` calls to Render |

Because Vercel forwards `/api` to the backend, the browser only ever talks to one address, so logins work without any cross-site cookie setup.

Do the parts in order. Allow about 30 minutes.

> **Free-plan behaviour:** Render puts the backend to sleep after 15 minutes without visitors. The next visitor sees a "Waking up the server" notice for a minute or two, and the page then refreshes by itself.

---

## Part 1: Database (Aiven)

1. Sign up at <https://console.aiven.io/signup>. No card is needed.
2. Click **Create service** and choose **MySQL**.
3. Choose the **Free** plan and a region near you (for India, Singapore or Mumbai if offered).
4. Give it a name such as `recuriq-db` and click **Create service**. Wait until its status is **Running**.
5. On the service's **Overview** page, find **Connection information** and note these four values:
   - **Host** (looks like `recuriq-db-yourname.aivencloud.com`)
   - **Port** (a number such as `12345`)
   - **User** (`avnadmin`)
   - **Password** (click the eye icon to show it)
6. Load the tables and demo data from your computer. In PowerShell, run this with your own `HOST` and `PORT`. It asks for the password from step 5 (nothing shows while you type), then prints `Subscriptions in the database: 42`.

   ```powershell
   D:\ProjectsDriveecuriq\scripts\load-demo-db.ps1 -DbHost HOST -Port PORT
   ```

   If PowerShell says scripts are disabled, run `Set-ExecutionPolicy -Scope Process Bypass` first, then try again.

---

## Part 2: Backend (Render)

1. Sign up at <https://dashboard.render.com/register> using **GitHub**, and allow it to see the `recuriq` repository.
2. Click **New** and choose **Blueprint**.
3. Pick the `recuriq` repository. Render reads `render.yaml` and shows one service, `recuriq-api`.
4. Fill in the three values it asks for, using your Aiven details:

   | Name | Value |
   |---|---|
   | `DB_URL` | `jdbc:mysql://HOST:PORT/subscription_billing?sslMode=REQUIRED` |
   | `DB_USERNAME` | `avnadmin` |
   | `DB_PASSWORD` | your Aiven password |

5. Click **Apply** (or **Deploy Blueprint**). The first build takes 5 to 10 minutes.
6. When the service shows **Live**, copy its address from the top of the page. It looks like `https://recuriq-api.onrender.com`.
7. Check it: open that address followed by `/api/customer/plans/all` in your browser. You should see a list of plans as text.

**To turn on the AI analyst (optional):** open the service, go to **Environment**, add `GROQ_API_KEY` with a free key from <https://console.groq.com/keys>, and save. Render redeploys by itself.

---

## Part 3: Frontend (Vercel)

1. **Check the backend address.** Open `frontend/vercel.json`. It contains `https://recuriq-api.onrender.com` on two lines. If your Render address from Part 2 is different (Render adds a suffix when the name is taken), change both lines to your address, then commit and push:

   ```bash
   git -C D:/ProjectsDrive/recuriq commit -am "Point the frontend at my Render backend"
   ```
   ```bash
   git -C D:/ProjectsDrive/recuriq push
   ```
2. Sign up at <https://vercel.com/signup> using **GitHub**.
3. Click **Add New** and choose **Project**, then **Import** the `recuriq` repository.
4. Set **Root Directory** to `frontend` (click **Edit** next to it). Vercel detects **Vite** by itself; leave the other settings alone.
5. Click **Deploy**. It takes about a minute.
6. Open the address Vercel gives you, for example `https://recuriq.vercel.app`.

---

## Try it

| Portal | Where | Sign in as |
|---|---|---|
| Finance (AI briefing, forecast, churn risk) | `/management/login` | `finance@streamflix.com` |
| Admin | `/management/login` | `admin@streamflix.com` |
| Support | `/management/login` | `support@streamflix.com` |
| Customer | **Sign in** on the home page | `kunal.desai@example.com` |

All demo accounts share the password written at the top of `backend/src/main/resources/schema.sql`.

Then add the link where people will see it:

- On GitHub, click the ⚙️ next to **About** on the repository page and paste it into **Website**.
- Add it near the top of `README.md`.

---

## Good to know

- **The demo logins are public** because they are in this repository. Anyone can sign in, including as admin, and change the demo data. Don't put real information in the demo. To restore the original data, run the `load-demo-db.ps1` command from Part 1 again.
- **Every push to `main` redeploys** both the backend (Render) and the frontend (Vercel).
- **Free plans can change.** If one of these services changes its free plan, the same three pieces work on any host that runs a Docker container, a MySQL 8 database and a static site.

## If something goes wrong

| What you see | Likely cause | Fix |
|---|---|---|
| Render deploy fails with `Communications link failure` | `DB_URL` is wrong | Check host and port, and that it ends with `?sslMode=REQUIRED` |
| Render deploy fails with `Access denied` | Wrong database user or password | Re-copy them from Aiven into Render's **Environment** tab |
| Render log says `Unknown database 'subscription_billing'` | Part 1 step 6 was skipped | Run the `load-demo-db.ps1` command |
| The site shows "Waking up the server" for more than 5 minutes | Vercel is pointing at the wrong backend address | Compare `frontend/vercel.json` with your Render address; check the Render service is **Live** |
| Pages load but signing in fails | The backend is rejecting your site's address | In Render's **Environment** tab, set `CORS_ALLOWED_ORIGINS` to your exact site address, e.g. `https://recuriq.vercel.app` |
| A page refresh on `/finance` shows "404" | Vercel's Root Directory isn't `frontend` | Fix it under the Vercel project's **Settings**, then redeploy |
