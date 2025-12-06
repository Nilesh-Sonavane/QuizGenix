// script.js

// 1. Sidebar Toggle Logic
function toggleSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (sidebar) {
        sidebar.classList.toggle('collapsed');
    }
}

// 2. GLOBAL LOADER LOGIC
document.addEventListener("DOMContentLoaded", function () {

    // Attach loader to all Forms
    const forms = document.querySelectorAll("form");
    forms.forEach(form => {
        form.addEventListener("submit", function () {
            // Only show if the form is valid (HTML5 validation passes)
            if (form.checkValidity()) {
                showLoader();
            }
        });
    });

    // Optional: Attach loader to specific "slow" links (like Generate buttons)
    const actionButtons = document.querySelectorAll(".btn-generate, .btn-submit, .btn-login");
    actionButtons.forEach(btn => {
        btn.addEventListener("click", function (e) {
            // If it's a link (<a>) and not a submit button
            if (this.tagName === 'A') {
                showLoader();
            }
        });
    });
});

function showLoader() {
    const loader = document.getElementById('global-loader');
    if (loader) {
        loader.style.display = 'flex';
    }
}

function hideLoader() {
    const loader = document.getElementById('global-loader');
    if (loader) {
        loader.style.display = 'none';
    }
}