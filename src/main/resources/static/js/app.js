// Навигация на планшетах и мобильных устройствах
document.addEventListener('DOMContentLoaded', function () {
    const toggleBtn = document.getElementById('sidebarToggle');
    const sidebar = document.getElementById('appSidebar');
    const backdrop = document.getElementById('sidebarBackdrop');

    if (toggleBtn && sidebar && backdrop) {
        const closeSidebar = () => {
            sidebar.classList.remove('show');
            backdrop.classList.remove('show');
        };
        toggleBtn.addEventListener('click', () => {
            sidebar.classList.toggle('show');
            backdrop.classList.toggle('show');
        });
        backdrop.addEventListener('click', closeSidebar);
    }
});

// Глобальные настройки Chart.js
if (typeof Chart !== 'undefined') {
    Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';
    Chart.defaults.color = '#4b5563';
}

function formatSom(value) {
    if (value === null || value === undefined || isNaN(value)) return '0,00 сом';
    return Number(value).toLocaleString('ru-RU', {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    }) + ' сом';
}
