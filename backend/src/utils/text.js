function normalizeText(text) {
  return (text || '')
    .toLowerCase()
    .normalize('NFD')
    .replace(/[\u0300-\u036f]/g, '')
    .trim();
}

function getGeneralScopedReply(question, appOverviewMessage) {
  const q = normalizeText(question);
  if (!q) {
    return null;
  }

  const greetingPatterns = [/^hola\b/, /^bon dia\b/, /^bona tarda\b/, /^bona nit\b/, /^hey\b/, /^ei\b/];
  if (greetingPatterns.some((pattern) => pattern.test(q))) {
    return 'Hola! Soc l’assistent de PosturAI. Si vols, et puc ajudar amb postura, ergonomia o dolor d’esquena/cervicals.';
  }

  const appScopePatterns = [
    /sobre que (pots|puc) parlar/,
    /de que (pots|puc) parlar/,
    /de que va aquesta app/,
    /que fas/,
    /com em pots ajudar/,
    /quina ajuda em pots donar/,
    /que es posturai/
  ];
  if (appScopePatterns.some((pattern) => pattern.test(q))) {
    return appOverviewMessage;
  }

  return null;
}

module.exports = {
  normalizeText,
  getGeneralScopedReply
};
