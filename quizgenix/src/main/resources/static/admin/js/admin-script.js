// Toggle Sidebar Size (Desktop)
function toggleDesktopSidebar() {
    const sidebar = document.getElementById('sidebar');
    if (window.innerWidth > 768) {
        sidebar.classList.toggle('collapsed');
    }
}

// Toggle Sidebar Visibility (Mobile)
function toggleMobileSidebar() {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay');

    sidebar.classList.toggle('active');
    overlay.classList.toggle('active');
}

// Close mobile sidebar when resizing to desktop
window.addEventListener('resize', function () {
    const sidebar = document.getElementById('sidebar');
    const overlay = document.getElementById('overlay');
    if (window.innerWidth > 768) {
        sidebar.classList.remove('active');
        overlay.classList.remove('active');
    }
});