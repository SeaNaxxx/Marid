// MUST be the first import of any stdio entry: everything that would print to
// stdout (console.log/info, chatty library init) is rerouted to stderr so the
// stdout byte stream stays a clean SocketProtocol frame channel.
const stderr = console.error.bind(console);

console.log = (...args: unknown[]) => stderr(...args);
console.info = (...args: unknown[]) => stderr(...args);
// console.warn/error already write to stderr.
