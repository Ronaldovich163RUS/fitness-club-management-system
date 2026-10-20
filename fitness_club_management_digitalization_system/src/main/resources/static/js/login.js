document.addEventListener("DOMContentLoaded", () => {
	const form = document.getElementById("loginForm");
	const errorBox = document.getElementById("loginFormError");
	const login = document.getElementById("login");
	const password = document.getElementById("password");

	function setError(msg, title = "Ошибка входа") {
		if (!errorBox) return;
		if (!msg) {
			errorBox.style.visibility = "hidden";
			errorBox.innerHTML = "";
			return;
		}
		errorBox.style.visibility = "visible";
		errorBox.innerHTML =
			`<span class="admin-alert-title">${title}</span><div class="admin-alert-body">${msg}</div>`;
	}

	const qs = new URLSearchParams(window.location.search);
	if (qs.get("error") === "1") {
		setError("Неверный логин/пароль или некорректная роль пользователя.");
	}
	if (qs.get("registered") === "1") {
		setError("Регистрация успешна. Теперь войдите в систему.", "Успех");
	}
	if (qs.get("choose_role") === "1") {
		const overlay = document.getElementById("role-select-overlay");
		if (overlay) {
			overlay.hidden = false;
			overlay.setAttribute("aria-hidden", "false");
		}
	}

	form?.addEventListener("submit", (e) => {
		setError("");

		// HTML5 validation first
		if (!form.checkValidity()) {
			e.preventDefault();
			form.reportValidity();
			return;
		}

		// Extra lightweight checks
		const loginVal = (login?.value || "").trim();
		if (!loginVal || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(loginVal)) {
			e.preventDefault();
			setError("Введите корректный email.");
			login.focus();
			return;
		}

		if (!password.value || password.value.length < 8) {
			e.preventDefault();
			setError("Пароль должен быть не менее 8 символов.");
			password.focus();
			return;
		}
	});
});

