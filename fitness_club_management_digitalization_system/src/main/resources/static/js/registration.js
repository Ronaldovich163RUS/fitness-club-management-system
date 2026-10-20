document.addEventListener("DOMContentLoaded", () => {
	const form = document.getElementById("registrationForm");
	const password = document.getElementById("password");
	const passwordConfirm = document.getElementById("confirm-password");
	const formError = document.getElementById("registrationFormError");
	const birthdate = document.getElementById("birthdate");
	const lastName = document.getElementById("last-name");
	const firstName = document.getElementById("first-name");
	const middleName = document.getElementById("middle-name");
	const phone = document.getElementById("phone");
	const email = document.getElementById("email");

	const NAME_RE = /^[А-ЯЁ][а-яё]*(?:[ '\-–—][А-ЯЁ][а-яё]*)*$/;
	const EMAIL_RE = /^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$/;
	const PASSWORD_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^A-Za-z0-9]).{8,}$/;

	function setFormError(msg, title = "Ошибка регистрации") {
		if (!formError) return;
		if (!msg) {
			formError.style.visibility = "hidden";
			formError.innerHTML = "";
			return;
		}
		formError.style.visibility = "visible";
		formError.innerHTML =
			`<span class="admin-alert-title">${title}</span><div class="admin-alert-body">${msg}</div>`;
		formError.scrollIntoView({ behavior: "smooth", block: "center" });
	}

	const qs = new URLSearchParams(window.location.search);
	if (qs.get("error") === "1") {
		setFormError("Не удалось зарегистрировать пользователя (email/телефон уже существует или данные некорректны).");
	}

	function normalizeRussianPhone(raw) {
		let digits = String(raw || "").replace(/\D/g, "");
		if (digits.length === 11 && digits.startsWith("8")) digits = "7" + digits.slice(1);
		if (digits.length === 10) digits = "7" + digits;
		return digits;
	}

	function validateNamePart(el, msg) {
		if (!el) return true;
		const v = (el.value || "").trim();
		if (!v) return false;
		if (v.length < 2 || v.length > 50) return false;
		if (!NAME_RE.test(v)) return false;
		return true;
	}

	function validateOptionalNamePart(el) {
		if (!el) return true;
		const v = (el.value || "").trim();
		if (!v) return true;
		return v.length >= 2 && v.length <= 50 && NAME_RE.test(v);
	}

	function validateBirthDate() {
		if (!birthdate) return true;
		const v = birthdate.value;
		if (!v) return false;
		const d = new Date(v + "T00:00:00");
		if (Number.isNaN(d.getTime())) return false;
		const min = new Date("1900-01-01T00:00:00");
		const now = new Date();
		if (d < min || d > now) return false;
		let age = now.getFullYear() - d.getFullYear();
		const m = now.getMonth() - d.getMonth();
		if (m < 0 || (m === 0 && now.getDate() < d.getDate())) age--;
		return age >= 14;
	}

	if (birthdate) {
		const now = new Date();
		const pad = (n) => String(n).padStart(2, "0");
		birthdate.max = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
	}

	function checkPasswords() {
		if (!password || !passwordConfirm) return true;

		if (!passwordConfirm.value) return true;

		const p1 = password.value;
		const p2 = passwordConfirm.value;

		if (p1 !== p2) {
			passwordConfirm.setCustomValidity("Пароли не совпадают.");
			setFormError("Пароли не совпадают.");
			return false;
		}

		passwordConfirm.setCustomValidity("");
		setFormError("");
		return true;
	}

	passwordConfirm?.addEventListener("input", () => {
		checkPasswords();
	});

	form?.addEventListener("submit", (e) => {
		setFormError("");
		if (!form.checkValidity()) {
			e.preventDefault();
			form.reportValidity();
			return;
		}

		if (!validateNamePart(lastName) || !validateNamePart(firstName) || !validateOptionalNamePart(middleName)) {
			e.preventDefault();
			setFormError("ФИО: 2–50 символов, кириллица, дефис/пробел/апостроф между частями.");
			(lastName || firstName)?.focus();
			return;
		}
		if (!EMAIL_RE.test((email?.value || "").trim().toLowerCase())) {
			e.preventDefault();
			setFormError("Некорректный формат email.");
			email?.focus();
			return;
		}
		if (!/^[0-9+\- ()]{10,20}$/.test((phone?.value || "").trim())
			|| !/^7\d{10}$/.test(normalizeRussianPhone(phone?.value || ""))) {
			e.preventDefault();
			setFormError("Телефон: российский номер, после нормализации формат +7 и 10 цифр.");
			phone?.focus();
			return;
		}
		if (!validateBirthDate()) {
			e.preventDefault();
			setFormError("Дата рождения: не раньше 01.01.1900, не в будущем, возраст не меньше 14 лет.");
			birthdate?.focus();
			return;
		}
		if (!PASSWORD_RE.test(password?.value || "")) {
			e.preventDefault();
			setFormError("Пароль от 8 символов: буквы верхнего и нижнего регистра, цифры и спецсимвол.");
			password?.focus();
			return;
		}

		if (!checkPasswords()) {
			e.preventDefault();
			passwordConfirm.focus();
			return;
		}
	});
});

