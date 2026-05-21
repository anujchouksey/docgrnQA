// RepoInsight Main JavaScript

document.addEventListener('DOMContentLoaded', function () {
    initSourceTypeToggle('dev');
    initSourceTypeToggle('qa');
    initSourceTypeToggle('target');
    initSidebarScrollSpy();
});

/**
 * Toggle between local and remote inputs for a given repo prefix (dev|qa|target).
 */
function initSourceTypeToggle(prefix) {
    const radios = document.querySelectorAll(`input[name="${prefix}SourceType"]`);
    if (!radios.length) return;

    const localGroup  = document.querySelector(`.${prefix}-local-group`);
    const remoteGroup = document.querySelector(`.${prefix}-remote-group`);

    function update(value) {
        if (!localGroup || !remoteGroup) return;
        if (value === 'LOCAL') {
            localGroup.classList.remove('hidden');
            remoteGroup.classList.add('hidden');
        } else {
            localGroup.classList.add('hidden');
            remoteGroup.classList.remove('hidden');
        }
    }

    radios.forEach(radio => {
        radio.addEventListener('change', () => update(radio.value));
        if (radio.checked) update(radio.value);
    });
}

/**
 * Highlight active sidebar nav item based on scroll position.
 */
function initSidebarScrollSpy() {
    const sidebarLinks = document.querySelectorAll('.sidebar-nav a[href^="#"]');
    if (!sidebarLinks.length) return;

    const observer = new IntersectionObserver(entries => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                const id = entry.target.getAttribute('id');
                sidebarLinks.forEach(link => {
                    link.classList.toggle('active', link.getAttribute('href') === '#' + id);
                });
            }
        });
    }, { threshold: 0.3 });

    sidebarLinks.forEach(link => {
        const id = link.getAttribute('href').slice(1);
        const target = document.getElementById(id);
        if (target) observer.observe(target);
    });
}

/**
 * Renders a simple SVG donut chart for coverage distribution.
 */
function renderCoverageChart(data) {
    const canvas = document.getElementById('coverageChart');
    if (!canvas) return;

    const total = data.covered + data.partial + data.notCovered + data.unclear;
    if (total === 0) return;

    const ctx = canvas.getContext('2d');
    const colors = ['#38a169', '#d69e2e', '#e53e3e', '#a0aec0'];
    const values = [data.covered, data.partial, data.notCovered, data.unclear];
    const labels = ['Covered', 'Partial', 'Not Covered', 'Unclear'];

    const cx = canvas.width / 2;
    const cy = canvas.height / 2;
    const radius = Math.min(cx, cy) - 20;
    const innerRadius = radius * 0.6;

    let startAngle = -Math.PI / 2;
    values.forEach((val, i) => {
        if (val === 0) return;
        const slice = (val / total) * 2 * Math.PI;
        ctx.beginPath();
        ctx.moveTo(cx, cy);
        ctx.arc(cx, cy, radius, startAngle, startAngle + slice);
        ctx.closePath();
        ctx.fillStyle = colors[i];
        ctx.fill();
        startAngle += slice;
    });

    // Inner circle (donut hole)
    ctx.beginPath();
    ctx.arc(cx, cy, innerRadius, 0, 2 * Math.PI);
    ctx.fillStyle = 'white';
    ctx.fill();

    // Center text
    ctx.fillStyle = '#2d3748';
    ctx.textAlign = 'center';
    ctx.font = 'bold 20px -apple-system, sans-serif';
    const pct = Math.round((data.covered + data.partial * 0.5) / total * 100);
    ctx.fillText(pct + '%', cx, cy + 7);
}

/**
 * Renders a simple bar chart for severity distribution.
 */
function renderSeverityChart(data) {
    const canvas = document.getElementById('severityChart');
    if (!canvas) return;
    // Simple placeholder – replace with full chart library if needed
    const ctx = canvas.getContext('2d');
    ctx.fillStyle = '#f7fafc';
    ctx.fillRect(0, 0, canvas.width, canvas.height);
    ctx.fillStyle = '#718096';
    ctx.textAlign = 'center';
    ctx.font = '12px sans-serif';
    ctx.fillText('Severity chart', canvas.width / 2, canvas.height / 2);
}
