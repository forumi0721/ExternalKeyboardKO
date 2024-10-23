import os

# 한자 파일을 처리하는 함수 (변환된 항목을 추가하면서 중복된 뜻을 병합)
def process_hanja_file(filepath, full):
    hanja_dict = {}  # 중복 처리를 위한 딕셔너리
    with open(filepath, 'r', encoding='utf-8') as file:
        for line in file:
            line = line.strip()
            # 공백과 # 제외
            if line and not line.startswith('#'):
                parts = line.split(':')
                if len(parts) >= 3:
                    hanja, hangul, meaning = parts[0], parts[1], ':'.join(parts[2:])
                    # 원래 항목 추가 (한글:한자:뜻)
                    key = f"{hanja}:{hangul}"

                    if not full and len(hangul) > 1:
                        continue

                    # 중복된 항목인지 확인
                    if key in hanja_dict:
                        # 뜻이 다르면 뜻을 병합 (중복된 뜻은 제외)
                        if meaning not in hanja_dict[key]:
                            hanja_dict[key] += f",{meaning}"
                    else:
                        hanja_dict[key] = meaning

                    # 한글이 한 글자일 경우 변환된 항목 추가 (한자:한글:뜻)
                    if len(hangul) == 1:
                        reverse_key = f"{hangul}:{hanja}"
                        if reverse_key in hanja_dict:
                            if meaning not in hanja_dict[reverse_key]:
                                hanja_dict[reverse_key] += f",{meaning}"
                        else:
                            hanja_dict[reverse_key] = meaning
    # 딕셔너리에서 병합된 결과를 반환
    return [f"{key}:{meaning}" for key, meaning in hanja_dict.items()]

# 일반 텍스트 파일을 처리하는 함수
def process_txt_file(filepath):
    processed_lines = []
    with open(filepath, 'r', encoding='utf-8') as file:
        for line in file:
            line = line.strip()
            # 공백과 # 제외
            if line and not line.startswith('#'):
                processed_lines.append(line)
    return processed_lines

# 실행 경로에서 txt 파일들을 처리하고 dictionary.txt를 제외한 모든 파일을 머지
def merge_txt_files(full):
    current_directory = os.getcwd()  # 현재 경로
    all_lines = []

    for filename in os.listdir(current_directory):
        if filename.endswith('.txt') and filename not in ['dictionary.txt', 'dictionary_full.txt', 'dictionary_min.txt']:
            filepath = os.path.join(current_directory, filename)
            if filename == 'hanja.txt':
                # hanja.txt 파일 처리
                all_lines.extend(process_hanja_file(filepath, full))
            else:
                # 다른 일반 텍스트 파일 처리
                all_lines.extend(process_txt_file(filepath))

    # 첫 번째 항목을 기준으로 이진 탐색이 가능하도록 정렬
    all_lines.sort(key=lambda x: x.split(':')[0])

    # 결과를 dictionary.txt로 저장
    file_name = 'dictionary_min.txt'
    if full:
        file_name = 'dictionary_full.txt'
    with open(file_name, 'w', encoding='utf-8') as output_file:
        for line in all_lines:
            output_file.write(line + '\n')

if __name__ == '__main__':
    merge_txt_files(True)
    merge_txt_files(False)

