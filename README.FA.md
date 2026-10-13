<div dir="rtl">

# Server-Talk

یک پلاگین سبک [Paper](https://papermc.io/) برای ماینکرافت **1.21.11** که دو قابلیت اصلی را در اختیار ادمین‌های سرور قرار می‌دهد:

- **ابزارهای پاک‌کردن چت** – پاک‌کردن چت برای همه یا فقط برای خودتان.
- **متن سه‌بعدی شناور و پایدار** – ساخت، استایل‌دهی، جابه‌جایی و حذف هولوگرام‌های متنی (ساخته‌شده با موجودیت‌های اصلی `TextDisplay` ماینکرافت) که بعد از ری‌استارت هم باقی می‌مانند، با سیستم ذخیره‌سازی قابل‌تعویض (`NONE`، `JSON` یا `MYSQL`).

بدون آرمور استند و بدون نیاز به هیچ پلاگین هولوگرام خارجی.

---

## ویژگی‌ها

- متن سه‌بعدی شناور با موجودیت‌های بومی `TextDisplay`
- پشتیبانی از کدهای رنگ و فرمت با `&` (مثلاً `&c&lHello`)
- متن چندخطی با استفاده از `|` به‌عنوان جداکننده‌ی خط
- تنظیمات مجزا برای هر متن: اندازه، رنگ و شفافیت پس‌زمینه، سایه، see-through و حالت billboard
- ویرایش با **ID** یا فقط با هدف‌گرفتن **نزدیک‌ترین** متن
- سه روش ذخیره‌سازی که در زمان اجرا قابل تغییر هستند و اطلاعات به‌طور خودکار منتقل می‌شود:
  - `NONE` – هیچ چیز ذخیره نمی‌شود
  - `JSON` – ذخیره در یک فایل داخل پوشه‌ی پلاگین
  - `MYSQL` – ذخیره در دیتابیس MySQL
- بازگشت ایمن: اگر ذخیره‌سازی تنظیم‌شده راه‌اندازی نشود، پلاگین به‌جای کرش‌کردن به حالت `NONE` برمی‌گردد
- متن‌ها هنگام لود شدن چانک‌ها به‌طور خودکار دوباره اسپاون می‌شوند و موجودیت‌های قدیمی و اضافی پاک می‌شوند

---

## پیش‌نیازها

| پیش‌نیاز | نسخه |
|----------|------|
| نرم‌افزار سرور | Paper 1.21.11 (یا سازگار با آن) |
| Java | 21 |
| MySQL (اختیاری) | هر سرور سازگار با MySQL، فقط در صورت استفاده از ذخیره‌سازی `MYSQL` |

---

## نصب

1. آخرین نسخه‌ی `server-talk-<version>.jar` را دانلود کنید (یا [خودتان بیلد کنید](#ساخت-از-روی-سورس)).
2. فایل jar را داخل پوشه‌ی `plugins/` سرورتان بگذارید.
3. سرور را اجرا (یا ری‌استارت) کنید.
4. فایل `configvk.yml` در مسیر `plugins/server-talk/` ساخته می‌شود. در صورت نیاز آن را تنظیم کنید.

---

## دستورات

| دستور | میانبرها | توضیح | پرمیشن |
|-------|----------|-------|--------|
| `/clearchat` | `/cc` | چت را برای **همه‌ی** بازیکنان پاک می‌کند | `servertalk.clearchat` |
| `/clearmassagechat` | `/cmc` | فقط چت **خودتان** را پاک می‌کند | *(ندارد)* |
| `/text3d [size] <text>` | – | یک متن سه‌بعدی شناور جلوی شما می‌سازد | `servertalk.text3d` |
| `/text3d remove` | – | نزدیک‌ترین متن سه‌بعدی را حذف می‌کند | `servertalk.text3d` |
| `/edittext3d ...` | `/et3d` | یک متن سه‌بعدی را ویرایش می‌کند (پایین‌تر ببینید) | `servertalk.edittext3d` |
| `/deletetext3d <id\|nearest\|all>` | `/dt3d` | متن(های) سه‌بعدی را حذف می‌کند | `servertalk.deletetext3d` (گزینه‌ی `all` به `servertalk.admin` هم نیاز دارد) |
| `/servertalk <storage\|reload\|list>` | `/st` | دستورات ادمین | `servertalk.admin` |

پیش‌فرض همه‌ی پرمیشن‌ها **OP** است.

### ساخت متن

```
/text3d <text>
/text3d <size> <text>
```

- `size` اختیاری است. مقدار پیش‌فرض `5` است و مقادیر بین `0.5` و `50` محدود می‌شوند.
- برای رفتن به خط بعد از `|` استفاده کنید.
- از کدهای رنگ `&` استفاده کنید.

**مثال:**

```
/text3d 6 &a&lWelcome to the server | &eHave fun!
```

هنگام ساخت، یک **ID** هشت‌کاراکتری (مثلاً `a1b2c3d4`) دریافت می‌کنید. در هر زمان با `/st list` می‌توانید همه‌ی IDها را ببینید.

### ویرایش متن

به سه روش می‌توانید یک متن را هدف بگیرید:

```
/et3d id <id> <action> ...     # فرم کامل
/et3d <id> <action> ...        # فرم کوتاه
/et3d <action> ...             # نزدیک‌ترین متن را هدف می‌گیرد (تا ۱۵ بلوک)
```

اجرای `/et3d` بدون آرگومان، یک منوی راهنمای کلیکی در چت نمایش می‌دهد.

| اکشن | نحوه‌ی استفاده | توضیح |
|------|----------------|-------|
| `info` | `/et3d id <id> info` | نمایش موقعیت، اندازه، billboard، سایه، پس‌زمینه و مالک |
| `text` | `/et3d id <id> text <new text>` | جایگزینی متن (`\|` = خط جدید، کدهای `&` پشتیبانی می‌شوند) |
| `color` | `/et3d id <id> color <&c&lText>` | جایگزینی متن با نسخه‌ی رنگی |
| `pos` | `/et3d id <id> pos <x> <y> <z>` | تعیین موقعیت دقیق |
| `move` | `/et3d id <id> move <dx> <dy> <dz>` | جابه‌جایی نسبت به موقعیت فعلی |
| `size` | `/et3d id <id> size <value>` | تغییر اندازه (0.5 تا 50) |
| `bg` | `/et3d id <id> bg <r> <g> <b> <a>` | تعیین رنگ پس‌زمینه (هر مقدار بین 0 تا 255) |
| `nobg` | `/et3d id <id> nobg` | شفاف‌کردن پس‌زمینه |
| `shadow` | `/et3d id <id> shadow <true\|false>` | روشن/خاموش‌کردن سایه‌ی متن |
| `seethrough` | `/et3d id <id> seethrough <true\|false>` | روشن/خاموش‌کردن see-through (دیده‌شدن از پشت بلوک‌ها) |
| `billboard` | `/et3d id <id> billboard <fixed\|vertical\|horizontal\|center>` | تعیین اینکه متن چطور به سمت بازیکن بچرخد |
| `delete` / `remove` | `/et3d id <id> delete` | حذف متن |

**مثال:**

```
/et3d id a1b2c3d4 bg 0 0 0 200
/et3d id a1b2c3d4 billboard vertical
/et3d id a1b2c3d4 move 0 1 0
```

### حذف متن

```
/dt3d <id>        # حذف یک متن مشخص
/dt3d nearest     # حذف نزدیک‌ترین متن
/dt3d all         # حذف همه‌ی متن‌ها (نیاز به servertalk.admin)
```

### دستورات ادمین

```
/st storage <none|json|mysql|status>   # تغییر روش ذخیره‌سازی یا نمایش روش فعلی
/st reload                             # ری‌لود configvk.yml
/st list                               # نمایش همه‌ی متن‌های لودشده با ID و موقعیت
```

هنگام تغییر روش ذخیره‌سازی، متن‌های موجود به روش جدید **کپی** می‌شوند و متن‌هایی که از قبل در آن ذخیره شده‌اند ایمپورت می‌شوند. حالت جدید در `configvk.yml` هم نوشته می‌شود.

---

## پرمیشن‌ها

| پرمیشن | توضیح | پیش‌فرض |
|--------|-------|---------|
| `servertalk.clearchat` | استفاده از `/clearchat` | OP |
| `servertalk.text3d` | استفاده از `/text3d` | OP |
| `servertalk.edittext3d` | استفاده از `/edittext3d` | OP |
| `servertalk.deletetext3d` | استفاده از `/deletetext3d` | OP |
| `servertalk.admin` | استفاده از `/servertalk` و `/dt3d all` | OP |

---

## تنظیمات

فایل `plugins/server-talk/configvk.yml`:

```yaml
# Storage mode: NONE | JSON | MYSQL
StorageMode: JSON

Json:
  FileName: 3dtexts.json

MySQL:
  Host: localhost
  Port: 3306
  Database: servertalk
  Username: root
  Password: ""
  Table: 3d_texts
  UseSSL: false
  PoolSize: 4
```

### روش‌های ذخیره‌سازی

| حالت | رفتار |
|------|-------|
| `NONE` | هیچ چیز ذخیره نمی‌شود. همه‌ی متن‌های سه‌بعدی بعد از ری‌استارت از بین می‌روند. |
| `JSON` | متن‌ها در `plugins/server-talk/<Json.FileName>` ذخیره می‌شوند. بدون نیاز به هیچ راه‌اندازی. |
| `MYSQL` | متن‌ها در جدول تنظیم‌شده ذخیره می‌شوند (جدول به‌طور خودکار با `utf8mb4` ساخته می‌شود). مناسب برای شبکه‌های چندسروری یا تعداد زیاد متن. |

> **نکته:** خود دیتابیس MySQL (`MySQL.Database`) باید از قبل وجود داشته باشد؛ پلاگین فقط جدول را می‌سازد.

---

## ساخت از روی سورس

پیش‌نیاز: **JDK 21**. Gradle wrapper (نسخه‌ی Gradle 9.4.0) داخل پروژه موجود است، پس نیازی به نصب Gradle نیست.

```bash
git clone https://github.com/BenyVK/Server-Talk.git
cd Server-Talk
./gradlew build        # در ویندوز: gradlew.bat build
```

فایل jar ساخته‌شده در `build/libs/` قرار می‌گیرد.

---

## ساختار پروژه

```
src/main/java/ir/gharri/servertalk/
├── ServerTalk.java      # کلاس اصلی پلاگین، دستورات و منطق متن سه‌بعدی
├── StoredText3D.java    # مدل داده‌ی یک متن سه‌بعدی ذخیره‌شده
├── TextStorage.java     # اینترفیس ذخیره‌سازی
├── NoneStorage.java     # ذخیره‌سازی بدون عملکرد (no-op)
├── JsonStorage.java     # ذخیره‌سازی در فایل JSON (Gson)
└── MySqlStorage.java    # ذخیره‌سازی در MySQL (JDBC)

src/main/resources/
├── plugin.yml           # دستورات و پرمیشن‌ها
└── configvk.yml         # تنظیمات پیش‌فرض
```

---

## مشارکت

Issue و Pull Request در [github.com/BenyVK/Server-Talk](https://github.com/BenyVK/Server-Talk) پذیرفته می‌شود.

---

## لایسنس

این پروژه تحت [لایسنس MIT](LICENSE) منتشر شده است.

---

ساخته‌شده توسط **BenyVK** ([Gharri.ir](https://gharri.ir))

## 👤 سازندگان

| | |
|---|---|
| **پلاگین** | Server Talk's |
| **سازنده** | Benyamin Gharri |
| **یوتیوب** | [BenyVKx](https://www.youtube.com/@BenyVKx) |
| **ویدیوی آموزشی** | به‌زودی |
| **README انگلیسی** | [README.md](https://github.com/BenyVK/Server-Talk/blob/main/README.md) |

</div>
