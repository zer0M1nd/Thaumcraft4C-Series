"""Read declarative legacy research calls without executing any legacy Java code."""
import json
import re


def uncomment(source):
    # Keep quoted text and preserve line numbers for review references.
    pattern = r'"(?:\\.|[^"\\])*"|\'(?:\\.|[^\'\\])*\'|//[^\n]*|/\*[\s\S]*?\*/'
    return re.sub(pattern, lambda match: ('\n' * match[0].count('\n') if match[0].startswith(('/',)) else match[0]), source)


def arguments(source, start):
    """Return comma-separated top-level arguments and index after closing delimiter."""
    opening = source[start]
    assert opening in '([{'
    stack = [opening]
    quote = None
    escape = False
    last = start + 1
    values = []
    for position in range(start + 1, len(source)):
        char = source[position]
        if quote:
            if escape:
                escape = False
            elif char == '\\':
                escape = True
            elif char == quote:
                quote = None
            continue
        if char in '\"\'':
            quote = char
        elif char in '([{':
            stack.append(char)
        elif char in ')]}':
            if '([{'.index(stack.pop()) != ')]}'.index(char):
                raise ValueError('Unbalanced Java expression')
            if not stack:
                tail = source[last:position].strip()
                if tail:
                    values.append(tail)
                return values, position + 1
        elif char == ',' and len(stack) == 1:
            values.append(source[last:position].strip())
            last = position + 1
    raise ValueError('Unterminated Java expression')


def research_calls(source):
    source = uncomment(source)
    results = []
    for match in re.finditer(r'new\s+ResearchItem\s*\(', source):
        args, end = arguments(source, match.end() - 1)
        calls = []
        while True:
            method = re.match(r'\s*\.\s*(\w+)\s*\(', source[end:])
            if not method:
                break
            values, end = arguments(source, end + method.end() - 1)
            calls.append({'method': method[1], 'arguments': values})
        if not any(row['method'] == 'registerResearchItem' for row in calls):
            continue
        results.append({'key': json.loads(args[0]), 'constructor': args, 'calls': calls,
                        'source_line': source[:match.start()].count('\n') + 1})
    return results
